package br.com.clinicaestetica.data;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Cofre dos termos assinados. Criptografa com AES-256-GCM usando uma chave que fica no
 * Android Keystore e nunca sai do aparelho. O id da paciente entra na autenticação, então
 * um termo copiado para o registro de outra paciente não abre.
 */
public final class ConsentVault {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "clinica_signature_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private static SecretKey testKey;

    private ConsentVault() {
    }

    /** Criptografa e devolve em Base64, pronto para gravar como texto no banco. */
    public static String seal(byte[] plain, long patientId) {
        return Base64.getEncoder().encodeToString(encrypt(plain, patientId));
    }

    public static byte[] open(String sealed, long patientId) throws GeneralSecurityException {
        byte[] stored;
        try {
            stored = Base64.getDecoder().decode(sealed);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Pacote do termo não é Base64 válido", e);
        }
        return decrypt(stored, patientId);
    }

    /** Formato binário: IV de 12 bytes seguido do texto cifrado com a etiqueta GCM. */
    static byte[] encrypt(byte[] plain, long patientId) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key());
            cipher.updateAAD(authData(patientId));
            byte[] sealed = cipher.doFinal(plain);
            return ByteBuffer.allocate(IV_BYTES + sealed.length)
                    .put(cipher.getIV())
                    .put(sealed)
                    .array();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Não foi possível criptografar o termo", e);
        }
    }

    static byte[] decrypt(byte[] stored, long patientId) throws GeneralSecurityException {
        if (stored == null || stored.length <= IV_BYTES) {
            throw new GeneralSecurityException("Pacote do termo vazio ou incompleto");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(),
                    new GCMParameterSpec(TAG_BITS, Arrays.copyOf(stored, IV_BYTES)));
            cipher.updateAAD(authData(patientId));
            return cipher.doFinal(stored, IV_BYTES, stored.length - IV_BYTES);
        } catch (IOException e) {
            throw new GeneralSecurityException(e);
        }
    }

    @VisibleForTesting
    public static void useTestKey(SecretKey key) {
        testKey = key;
    }

    private static byte[] authData(long patientId) {
        return ByteBuffer.allocate(Long.BYTES).putLong(patientId).array();
    }

    private static SecretKey key() throws GeneralSecurityException, IOException {
        if (testKey != null) {
            return testKey;
        }
        KeyStore store = KeyStore.getInstance(KEYSTORE);
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) {
            return (SecretKey) store.getKey(KEY_ALIAS, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
