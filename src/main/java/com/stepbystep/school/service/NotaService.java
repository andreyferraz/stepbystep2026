package com.stepbystep.school.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import com.stepbystep.school.model.Aluno;
import com.stepbystep.school.model.Nota;
import com.stepbystep.school.repository.NotaRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class NotaService {

    private record PdfWriteResult(PDPageContentStream stream, float y) {
    }

    public static class ResumoTurmaNotas {
        private final String turmaNome;
        private final double mediaNotas;
        private final double mediaPresenca;
        private final long alunosMediaBaixa;
        private final long alunosAbaixoPresenca;

        public ResumoTurmaNotas(
            String turmaNome,
            double mediaNotas,
            double mediaPresenca,
            long alunosMediaBaixa,
            long alunosAbaixoPresenca
        ) {
            this.turmaNome = turmaNome;
            this.mediaNotas = mediaNotas;
            this.mediaPresenca = mediaPresenca;
            this.alunosMediaBaixa = alunosMediaBaixa;
            this.alunosAbaixoPresenca = alunosAbaixoPresenca;
        }

        public String getTurmaNome() {
            return turmaNome;
        }

        public double getMediaNotas() {
            return mediaNotas;
        }

        public double getMediaPresenca() {
            return mediaPresenca;
        }

        public long getAlunosMediaBaixa() {
            return alunosMediaBaixa;
        }

        public long getAlunosAbaixoPresenca() {
            return alunosAbaixoPresenca;
        }

        public long getTotalAlertas() {
            return alunosMediaBaixa + alunosAbaixoPresenca;
        }
    }

    private static final String CAMPO_ID_ALUNO = "ID do aluno";
    private static final String CAMPO_ID_NOTA = "ID da nota";
    private static final String CAMPO_NOTA = "Nota";
    private static final String CAMPO_BIMESTRE = "Bimestre";
    private static final String CAMPO_ATIVIDADE = "Atividade";
    private static final String CAMPO_PRESENCA = "Presença";
    private static final String CAMPO_DATA_REFERENCIA = "Data da aula";
    private static final String MSG_NOTA_NAO_ENCONTRADA = "Nota não encontrada com ID: ";
    private static final String PRESENCA_PRESENTE = "PRESENTE";

    private final AlunoService alunoService;
    private final NotaRepository notaRepository;

    public NotaService(AlunoService alunoService, NotaRepository notaRepository) {
        this.alunoService = alunoService;
        this.notaRepository = notaRepository;
    }

    public Nota cadastrarNota(UUID alunoId, Nota nota) {
        ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
        ValidationUtils.validarCampoObrigatorio(nota, CAMPO_NOTA);
        ValidationUtils.validarCampoObrigatorio(nota.getDataReferencia(), CAMPO_DATA_REFERENCIA);

        boolean temNota = nota.getValor() != null;
        boolean temPresenca = contemTexto(nota.getPresenca());

        if (!temNota && !temPresenca) {
            throw new IllegalArgumentException("Informe ao menos a nota ou a presença.");
        }

        if (temNota) {
            ValidationUtils.validarCampoObrigatorio(nota.getBimestre(), CAMPO_BIMESTRE);
            ValidationUtils.validarCampoStringObrigatorio(nota.getAtividade(), CAMPO_ATIVIDADE);
            validarIntervaloBimestre(nota.getBimestre());
            validarIntervaloNota(nota.getValor());
        }

        if (temPresenca) {
            ValidationUtils.validarCampoStringObrigatorio(nota.getPresenca(), CAMPO_PRESENCA);
        }

        if (!temNota && nota.getBimestre() == null) {
            nota.setBimestre(calcularBimestrePorData(nota.getDataReferencia()));
        }

        Aluno aluno = alunoService.obterAlunoPorId(alunoId);
        nota.setAluno(aluno);
        nota.setAtividade(temNota ? nota.getAtividade().trim() : normalizarTextoOpcional(nota.getAtividade()));
        nota.setPresenca(temPresenca ? normalizarPresenca(nota.getPresenca()) : null);
        nota.setDescricao(normalizarTextoOpcional(nota.getDescricao()));

        return notaRepository.save(nota);
    }

    public Nota editarNota(UUID notaId, Nota nota) {
        ValidationUtils.validarCampoObrigatorio(notaId, CAMPO_ID_NOTA);
        ValidationUtils.validarCampoObrigatorio(nota, CAMPO_NOTA);
        ValidationUtils.validarCampoObrigatorio(nota.getDataReferencia(), CAMPO_DATA_REFERENCIA);

        boolean temNota = nota.getValor() != null;
        boolean temPresenca = contemTexto(nota.getPresenca());

        if (!temNota && !temPresenca) {
            throw new IllegalArgumentException("Informe ao menos a nota ou a presença.");
        }

        if (temNota) {
            ValidationUtils.validarCampoObrigatorio(nota.getBimestre(), CAMPO_BIMESTRE);
            ValidationUtils.validarCampoStringObrigatorio(nota.getAtividade(), CAMPO_ATIVIDADE);
            validarIntervaloBimestre(nota.getBimestre());
            validarIntervaloNota(nota.getValor());
        }

        if (temPresenca) {
            ValidationUtils.validarCampoStringObrigatorio(nota.getPresenca(), CAMPO_PRESENCA);
        }

        Nota notaExistente = notaRepository.findById(notaId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_NOTA_NAO_ENCONTRADA + notaId));

        notaExistente.setValor(nota.getValor());
        notaExistente.setBimestre(nota.getBimestre());
        notaExistente.setAtividade(temNota ? nota.getAtividade().trim() : normalizarTextoOpcional(nota.getAtividade()));
        notaExistente.setPresenca(temPresenca ? normalizarPresenca(nota.getPresenca()) : null);
        notaExistente.setDataReferencia(nota.getDataReferencia());
        notaExistente.setDescricao(normalizarTextoOpcional(nota.getDescricao()));

        return notaRepository.save(notaExistente);
    }

    public List<Nota> listarNotasFiltradas(String termoBusca, UUID turmaId, Integer bimestre) {
        String termoNormalizado = termoBusca == null ? "" : termoBusca.trim().toLowerCase(Locale.ROOT);

        return notaRepository.findAllByOrderByDataReferenciaDescIdDesc().stream()
            .filter(nota -> turmaId == null
                || (nota.getAluno() != null
                    && nota.getAluno().getTurma() != null
                    && turmaId.equals(nota.getAluno().getTurma().getId())))
            .filter(nota -> bimestre == null || Objects.equals(bimestre, nota.getBimestre()))
            .filter(nota -> termoNormalizado.isEmpty()
                || contemTexto(nota.getAtividade(), termoNormalizado)
                || contemTexto(nota.getAluno() == null ? null : nota.getAluno().getNome(), termoNormalizado)
                || contemTexto(nota.getAluno() == null || nota.getAluno().getTurma() == null
                    ? null
                    : nota.getAluno().getTurma().getNome(), termoNormalizado))
            .sorted(Comparator.comparing(Nota::getDataReferencia,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    public double calcularMediaNotas(List<Nota> notas) {
        return notas.stream()
            .map(Nota::getValor)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }

    public double calcularMediaPresenca(List<Nota> notas) {
        List<Nota> notasComPresenca = notas.stream()
            .filter(nota -> contemTexto(nota.getPresenca()))
            .toList();

        if (notasComPresenca.isEmpty()) {
            return 0.0;
        }

        long presentes = notasComPresenca.stream()
            .filter(nota -> PRESENCA_PRESENTE.equalsIgnoreCase(nota.getPresenca()))
            .count();

        return (presentes * 100.0) / notasComPresenca.size();
    }

    public long contarAlunosAbaixoDePresenca(List<Nota> notas, double percentualMinimo) {
        Map<UUID, List<Nota>> notasPorAluno = notas.stream()
            .filter(nota -> contemTexto(nota.getPresenca()))
            .filter(nota -> nota.getAluno() != null && nota.getAluno().getId() != null)
            .collect(Collectors.groupingBy(nota -> nota.getAluno().getId()));

        if (notasPorAluno.isEmpty()) {
            return 0;
        }

        return notasPorAluno.values().stream()
            .filter(listaAluno -> {
                long presentes = listaAluno.stream()
                    .filter(nota -> PRESENCA_PRESENTE.equalsIgnoreCase(nota.getPresenca()))
                    .count();

                double percentual = (presentes * 100.0) / listaAluno.size();
                return percentual < percentualMinimo;
            })
            .count();
    }

    public List<Nota> listarLancamentosDoDia(List<Nota> notas, LocalDate data) {
        return notas.stream()
            .filter(nota -> contemTexto(nota.getPresenca()))
            .filter(nota -> nota.getDataReferencia() != null && nota.getDataReferencia().isEqual(data))
            .sorted(Comparator.comparing(nota -> nota.getAluno() == null ? "" : nota.getAluno().getNome(),
                String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public List<ResumoTurmaNotas> listarResumoTurmasEmAtencao(
        List<Nota> notas,
        double mediaMinima,
        double presencaMinima
    ) {
        Map<String, List<Nota>> notasPorTurma = notas.stream()
            .filter(nota -> nota.getAluno() != null && nota.getAluno().getTurma() != null)
            .collect(Collectors.groupingBy(nota -> nota.getAluno().getTurma().getNome()));

        return notasPorTurma.entrySet().stream()
            .map(entry -> {
                String turmaNome = entry.getKey();
                List<Nota> notasTurma = entry.getValue();

                double mediaNotasTurma = calcularMediaNotas(notasTurma);
                double mediaPresencaTurma = calcularMediaPresenca(notasTurma);

                Map<UUID, List<Nota>> notasPorAluno = notasTurma.stream()
                    .filter(nota -> nota.getAluno() != null && nota.getAluno().getId() != null)
                    .collect(Collectors.groupingBy(nota -> nota.getAluno().getId()));

                long alunosMediaBaixa = notasPorAluno.values().stream()
                    .filter(listaAluno -> calcularMediaNotas(listaAluno) < mediaMinima)
                    .count();

                long alunosAbaixoPresenca = notasPorAluno.values().stream()
                    .filter(listaAluno -> calcularMediaPresenca(listaAluno) < presencaMinima)
                    .count();

                return new ResumoTurmaNotas(
                    turmaNome,
                    mediaNotasTurma,
                    mediaPresencaTurma,
                    alunosMediaBaixa,
                    alunosAbaixoPresenca
                );
            })
            .filter(resumo -> resumo.getMediaNotas() < mediaMinima
                || resumo.getMediaPresenca() < presencaMinima
                || resumo.getTotalAlertas() > 0)
            .sorted(Comparator
                .comparingLong(ResumoTurmaNotas::getTotalAlertas).reversed()
                .thenComparingDouble(ResumoTurmaNotas::getMediaPresenca)
                .thenComparingDouble(ResumoTurmaNotas::getMediaNotas))
            .limit(5)
            .toList();
    }

    public byte[] gerarBoletimPdf(List<Nota> notas, String notaBusca, UUID turmaId, Integer bimestre) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margem = 40f;
            float y = page.getMediaBox().getHeight() - margem;
            float espacamento = 15f;

            PDPageContentStream stream = new PDPageContentStream(document, page);
            stream.setLeading(espacamento);
            stream.beginText();
            stream.newLineAtOffset(margem, y);

            y = escreverLinha(stream, y, "Boletim - Step by Step", PDType1Font.HELVETICA_BOLD, 14, espacamento);
            y = escreverLinha(stream, y,
                "Gerado em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                PDType1Font.HELVETICA, 10, espacamento);
            y = escreverLinha(stream, y,
                "Filtros: busca='" + textoFiltro(notaBusca) + "', turma='" + textoFiltro(turmaId == null ? null : turmaId.toString())
                    + "', bimestre='" + textoFiltro(bimestre == null ? null : bimestre.toString()) + "'",
                PDType1Font.HELVETICA_OBLIQUE, 10, espacamento);
            y = escreverLinha(stream, y, " ", PDType1Font.HELVETICA, 10, espacamento);

            PdfWriteResult resultado = escreverConteudoBoletim(document, stream, notas, margem, espacamento, y);
            resultado.stream().endText();
            resultado.stream().close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Não foi possível gerar o PDF do boletim.");
        }
    }
     
    public void excluirNota(UUID notaId) {
        ValidationUtils.validarCampoObrigatorio(notaId, CAMPO_ID_NOTA);
        if (!notaRepository.existsById(notaId)) {
            throw new IllegalArgumentException(MSG_NOTA_NAO_ENCONTRADA + notaId);
        }
        notaRepository.deleteById(notaId);
    }

    private void validarIntervaloBimestre(Integer bimestre) {
        if (bimestre < 1 || bimestre > 4) {
            throw new IllegalArgumentException("Bimestre deve ser entre 1 e 4");
        }
    }

    private void validarIntervaloNota(Double valor) {
        if (valor == null || valor < 0.0 || valor > 10.0) {
            throw new IllegalArgumentException("Valor da nota deve estar entre 0 e 10");
        }
    }

    private String normalizarPresenca(String presenca) {
        String presencaNormalizada = presenca == null ? "" : presenca.trim().toUpperCase(Locale.ROOT);
        if (!PRESENCA_PRESENTE.equals(presencaNormalizada)
            && !"FALTA".equals(presencaNormalizada)
            && !"JUSTIFICADA".equals(presencaNormalizada)) {
            throw new IllegalArgumentException("Presença inválida. Use: presente, falta ou justificada.");
        }

        return presencaNormalizada;
    }

    private String normalizarTextoOpcional(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private int calcularBimestrePorData(LocalDate data) {
        int mes = data == null ? LocalDate.now().getMonthValue() : data.getMonthValue();
        return ((mes - 1) / 3) + 1;
    }

    private String textoFiltro(String valor) {
        return contemTexto(valor) ? valor.trim() : "todos";
    }

    private String limitarTexto(String valor, int tamanhoMaximo) {
        if (valor == null) {
            return "";
        }

        if (valor.length() <= tamanhoMaximo) {
            return valor;
        }

        return valor.substring(0, Math.max(0, tamanhoMaximo - 3)) + "...";
    }

    private PdfWriteResult escreverConteudoBoletim(
        PDDocument document,
        PDPageContentStream stream,
        List<Nota> notas,
        float margem,
        float espacamento,
        float y
    ) throws IOException {
        if (notas.isEmpty()) {
            float novoY = escreverLinha(
                stream,
                y,
                "Nenhum lançamento encontrado para os filtros informados.",
                PDType1Font.HELVETICA,
                11,
                espacamento
            );
            return new PdfWriteResult(stream, novoY);
        }

        float cursorY = escreverLinha(
            stream,
            y,
            "Aluno | Turma | Atividade | Nota | Presenca | Data",
            PDType1Font.HELVETICA_BOLD,
            10,
            espacamento
        );
        PDPageContentStream streamAtual = stream;

        for (Nota nota : notas) {
            if (cursorY <= 70f) {
                streamAtual.endText();
                streamAtual.close();

                PDPage paginaAtual = new PDPage(PDRectangle.A4);
                document.addPage(paginaAtual);
                cursorY = paginaAtual.getMediaBox().getHeight() - margem;

                streamAtual = new PDPageContentStream(document, paginaAtual);
                streamAtual.setLeading(espacamento);
                streamAtual.beginText();
                streamAtual.newLineAtOffset(margem, cursorY);
                cursorY = escreverLinha(
                    streamAtual,
                    cursorY,
                    "Aluno | Turma | Atividade | Nota | Presenca | Data",
                    PDType1Font.HELVETICA_BOLD,
                    10,
                    espacamento
                );
            }

            cursorY = escreverLinha(
                streamAtual,
                cursorY,
                limitarTexto(montarLinhaLancamento(nota), 110),
                PDType1Font.HELVETICA,
                10,
                espacamento
            );
        }

        return new PdfWriteResult(streamAtual, cursorY);
    }

    private String montarLinhaLancamento(Nota nota) {
        String aluno = nota.getAluno() == null ? "-" : textoFiltro(nota.getAluno().getNome());
        String turma = nota.getAluno() == null || nota.getAluno().getTurma() == null
            ? "-"
            : textoFiltro(nota.getAluno().getTurma().getNome());
        String atividade = textoFiltro(nota.getAtividade());
        String valor = nota.getValor() == null ? "-" : String.format(Locale.forLanguageTag("pt-BR"), "%.1f", nota.getValor());
        String presenca = textoFiltro(nota.getPresenca());
        String data = nota.getDataReferencia() == null
            ? "-"
            : nota.getDataReferencia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        return aluno + " | " + turma + " | " + atividade + " | " + valor + " | " + presenca + " | " + data;
    }

    private float escreverLinha(
        PDPageContentStream stream,
        float y,
        String texto,
        PDType1Font fonte,
        int tamanhoFonte,
        float espacamento
    ) throws IOException {
        stream.setFont(fonte, tamanhoFonte);
        stream.showText(texto == null ? "" : texto);
        stream.newLine();
        return y - espacamento;
    }

    private boolean contemTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private boolean contemTexto(String origem, String termoNormalizado) {
        return origem != null && origem.toLowerCase(Locale.ROOT).contains(termoNormalizado);
    }
}
