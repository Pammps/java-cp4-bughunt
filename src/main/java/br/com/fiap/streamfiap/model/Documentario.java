package br.com.fiap.streamfiap.model;

import jakarta.persistence.Entity;

@Entity
public class Documentario extends Conteudo {

    private static final double PRECO_GRATUITO = 0.0;

    private String tema;

    public Documentario() {
    }

    public Documentario(String titulo, String categoria, int duracaoMinutos, int classificacaoEtaria, boolean disponivel, String tema) {
        super(titulo, categoria, duracaoMinutos, classificacaoEtaria, disponivel);
        this.tema = tema;
    }

    // documentario e gratuito e nao participa de promocao
    @Override
    public double calcularPrecoAluguel() {
        return PRECO_GRATUITO;
    }

    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }
}
