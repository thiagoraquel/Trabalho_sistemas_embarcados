package com.thiago.sgai.repository;

import com.thiago.sgai.model.RegraAutomacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegraAutomacaoRepository extends JpaRepository<RegraAutomacao, Long> {
    // Busca todas as regras ativas de uma sala específica
    List<RegraAutomacao> findBySalaIdAndAtivaTrue(String salaId);
}