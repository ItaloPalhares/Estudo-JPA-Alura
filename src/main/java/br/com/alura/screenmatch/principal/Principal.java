package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.Categoria;
import br.com.alura.screenmatch.model.DadosSerie;
import br.com.alura.screenmatch.model.DadosTemporada;
import br.com.alura.screenmatch.model.Episodio;
import br.com.alura.screenmatch.model.Serie;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";
    private List<DadosSerie> listaSeries = new ArrayList<>();
    private SerieRepository repository;
    private List<Serie> series = new ArrayList<>();
    private Optional<Serie> serieBusca;
    public Principal(SerieRepository repository) {
        this.repository = repository;
    }

    public void exibeMenu() {
        var loopOpcao = -1;
        while (loopOpcao != 0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar Séries pesquisadas
                    4 - Buscar Série por Titulo
                    5 - Buscar Série por atores
                    6 - Top 5 Séries cadastradas
                    7 - Buscar Séries por gênero
                    8 - Buscar Séries por maximo de temporadas e avaliação
                    9 - Buscar episódios por trecho
                    10 - Buscar top 5 episódios
                    11 - Buscar episódio por ano de lançamento

                    0 - Sair
                    """;

            System.out.println(menu);
            var opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSeries();
                    break;
                case 3:
                    imprimeLista();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtores();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorGenero();
                    break;
                case 8:
                    buscarSerieMaxTempMinAvaliacao();
                    break;
                case 9:
                    buscarEpisodoPorTrecho();
                    break;
                case 10:
                    buscarTop5Episodios();
                    break;
                case 11:
                    buscarEpisodioAPartirDeAno();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    loopOpcao = 0;
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    



    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        repository.save(serie);

        // listaSeries.add(dados);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void imprimeLista() {
        series = repository.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarEpisodioPorSeries(){
        imprimeLista();
        System.out.println("Digite o nome da série em que deseja buscar o episódio: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> opSerie = series.stream()
            .filter(s -> s.getTitulo().toLowerCase().contains(nomeSerie.toLowerCase()))
            .findFirst();
            
        if (opSerie.isPresent()){   
            var serieEncontrada = opSerie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
            var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
            DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
            temporadas.add(dadosTemporada);
                }
            temporadas.forEach(System.out::println);
            
           List<Episodio> episodios = temporadas.stream()
                .flatMap(d -> d.episodios().stream()
                    .map(e -> new Episodio(d.numero(), e)))
                .collect(Collectors.toList());
            
            serieEncontrada.setEpisodios(episodios);    
            repository.save(serieEncontrada);
            } else{
        System.out.println("Série não encontrada, experimente busca-lá no menu principal.");
        }
    
    }
    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma série pelo nome> ");
        var nomeSerie = leitura.nextLine();
        serieBusca = repository.findByTituloContainingIgnoreCase(nomeSerie);

        if(serieBusca.isPresent()){
            System.out.println("Dados da série: \n" + serieBusca.get());
        } else{
            System.out.println("Série não encontrada");
        }
    }

      private void buscarSeriePorAtores() {
        System.out.println("Qual o nome do ator que deseja buscar?");
        var nomeAtor = leitura.nextLine();
        System.out.println("avaliações a partir de que valor? ");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repository.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        System.out.println("Séries em que "+ nomeAtor + " trabalhou: ");
        seriesEncontradas.forEach(s-> System.out.println(s.getTitulo()+ " avaliação: " + s.getAvaliacao()));
    }

     private void buscarTop5Series() {
        List<Serie> serieTop5 = repository.findTop5ByOrderByAvaliacaoDesc();
        serieTop5.forEach(s-> System.out.println(s.getTitulo() + " - avaliação: " + s.getAvaliacao()));

        
    }

       private void buscarSeriesPorGenero() {
        System.out.println("Deseja buscar séries por qual categoria/gênero? ");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repository.findBygenero(categoria);
        System.out.println("Séries da categoria "+ nomeGenero+" :");
        seriesPorCategoria.forEach(System.out::println);
        
    }
    private void buscarSerieMaxTempMinAvaliacao() {
        System.out.println("Qual o limite de temporadas desejado?");
        var totalTemporadas = leitura.nextInt();
        System.out.println("Qual a avaliação minima desejada? ");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repository.seriesPorTemporadaEAvalicao(totalTemporadas, avaliacao);
        System.out.println("Séries que atendem aos requisito: ");
        seriesEncontradas.forEach(s-> System.out.println(s.getTitulo() + " - Temporadas: "+ s.getTotalTemporadas() + " - Avalição: " + s.getAvaliacao()));
           
    }

    private void buscarEpisodoPorTrecho() {
        System.out.println("Qual o titulo do episódio desejado?");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodiosEncontrados = repository.episodioPorTrecho(trechoEpisodio);
        episodiosEncontrados.forEach(e->
                System.out.printf("Série: %s Temporada %s - Episódio %s - %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(),e.getTitulo()));

    }

      private void buscarTop5Episodios() {
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            List<Episodio> top5Episodios = repository.topEpisodiosPorSerie(serie);
            top5Episodios.forEach(e->
                System.out.printf("Série: %s Temporada %s - Episódio %s - %s - Avaliacão %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(),e.getTitulo(), e.getAvaliacao()));

        } else { System.out.println("Episódios não encontrados, tente novamente!");}
        
    }

    private void buscarEpisodioAPartirDeAno(){
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            System.out.println("Deseja encontrar episódios a partir de que ano?");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();

            List<Episodio> episodiosAno = repository.episodiosAPartirDeAno(serie, anoLancamento);
            episodiosAno.forEach(System.out::println);
        }
    }

}