package br.com.clinicaestetica.data;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Criptografa as assinaturas com AES-256-GCM. A chave fica no Android Keystore e nunca sai
 * do aparelho. O id da paciente entra na autenticação, então a imagem de uma paciente não
 * abre se for copiada para o registro de outra.
 */
public final class SignatureVault {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "clinica_signature_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private static SecretKey testKey;

    private SignatureVault() {
    }

    /** Formato gravado no banco: IV de 12 bytes seguido do texto cifrado. */
    public static byte[] encrypt(byte[] plain, long patientId) {
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
            throw new IllegalStateException("Não foi possível criptografar a assinatura", e);
        }
    }

    public static byte[] decrypt(byte[] stored, long patientId) throws GeneralSecurityException {
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
