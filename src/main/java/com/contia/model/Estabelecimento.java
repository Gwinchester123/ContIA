package com.contia.model;

import jakarta.persistence.*;

@Entity
@Table(name = "estabelecimentos")
public class Estabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 80)
    private String tipo;

    @ManyToOne
    @JoinColumn(name = "proprietario_id", nullable = false)
    private Proprietario proprietario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanoEstabelecimento plano = PlanoEstabelecimento.BASICO;

    public Estabelecimento() {
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Proprietario getProprietario() {
        return proprietario;
    }

    public void setProprietario(Proprietario proprietario) {
        this.proprietario = proprietario;
    }

    public PlanoEstabelecimento getPlano() { return plano; }
    public void setPlano(PlanoEstabelecimento plano) { this.plano = plano; }
}
