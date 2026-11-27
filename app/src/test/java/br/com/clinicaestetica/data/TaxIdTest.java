package br.com.clinicaestetica.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** CPF e CNPJ (numérico e alfanumérico) pelos dígitos verificadores, sem internet. */
public class TaxIdTest {
    @Test
    public void validatesCpf() {
        assertTrue(TaxId.isValid("529.982.247-25"));
        assertTrue(TaxId.isValid("52998224725"));
        assertFalse(TaxId.isValid("529.982.247-24"));
        assertFalse("todos iguais", TaxId.isValid("111.111.111-11"));
        assertFalse("incompleto", TaxId.isValid("529.982.247"));
        assertEquals(TaxId.Kind.CPF, TaxId.kind("529.982.247-25"));
    }

    @Test
    public void validatesNumericCnpj() {
        assertTrue(TaxId.isValid("11.222.333/0001-81"));
        assertTrue(TaxId.isValid("11222333000181"));
        assertFalse(TaxId.isValid("11.222.333/0001-80"));
        assertFalse("todos iguais", TaxId.isValid("00000000000000"));
        assertEquals(TaxId.Kind.CNPJ, TaxId.kind("11222333000181"));
    }

    /** Exemplo oficial da Receita para o CNPJ alfanumérico (IN RFB 2.229/2024). */
    @Test
    public void validatesAlphanumericCnpj() {
        assertTrue(TaxId.isValid("12.ABC.345/01DE-35"));
        assertTrue("aceita minúsculas", TaxId.isValid("12abc34501de35"));
        assertFalse(TaxId.isValid("12.ABC.345/01DE-36"));
        assertFalse("verificadores são números", TaxId.isValid("12ABC34501DEA5"));
        assertEquals(TaxId.Kind.CNPJ, TaxId.kind("12A"));
    }

    @Test
    public void masksWhileTyping() {
        assertEquals("529", TaxId.format("529"));
        assertEquals("529.9", TaxId.format("5299"));
        assertEquals("529.982.247-25", TaxId.format("52998224725"));
        assertEquals("11.222.333/0001-81", TaxId.format("112223330001815555"));
        assertEquals("12.ABC.345/01DE-35", TaxId.format("12abc34501de35"));
        assertEquals("", TaxId.format(null));
    }

    @Test
    public void describesForTheField() {
        assertEquals("", TaxId.describe(""));
        assertEquals("CPF válido", TaxId.describe("529.982.247-25"));
        assertEquals("CNPJ inválido: confira os números", TaxId.describe("11222333000180"));
        assertTrue(TaxId.describe("5299").startsWith("Continue digitando"));
    }
}
