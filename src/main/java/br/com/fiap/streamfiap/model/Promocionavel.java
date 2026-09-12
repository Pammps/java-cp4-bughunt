package br.com.fiap.streamfiap.model;

/**
 * Contrato de promoção do StreamFIAP.
 * Toda classe que implementa esta interface deve aplicar
 * 20% de desconto sobre o preço informado.
 */
public interface Promocionavel {

    /** Multiplicador equivalente a 20% de desconto. */
    double MULTIPLICADOR_PROMOCAO = 0.8;

    double aplicarPromocao(double preco);
}
