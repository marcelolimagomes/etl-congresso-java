package br.leg.congresso.etl.pagegen.dto;

import lombok.Builder;
import lombok.Value;

/**
 * DTO de autor para renderização de página estática.
 */
@Value
@Builder
public class AutorDTO {
    String nome;
    /** "Deputado Federal" | "Senador" | "Comissão" | etc. */
    String tipo;
    /** "camara" | "senado" | null quando não é parlamentar */
    String casa;
    /** ID original do parlamentar na API de origem (null para entidades) */
    String idOriginal;
    /** URL de perfil resolvida para renderização da página estática, quando disponível */
    String perfilUrl;
    /** true se for o proponente/primeiro autor */
    boolean proponente;
    String partido;
    String uf;
}
