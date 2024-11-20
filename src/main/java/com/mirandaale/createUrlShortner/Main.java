package com.mirandaale.createUrlShortner;

// Importa as bibliotecas necessárias para a função Lambda.
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Define a classe principal que implementa a interface RequestHandler.
// Esta interface é necessária para funções Lambda que recebem um evento de entrada e retornam uma resposta.
// <Map<String, Object>, Map<String, String>> define os tipos de entrada e saída da função Lambda.
public class Main implements RequestHandler<Map<String, Object>, Map<String, String>> {

    // Cria uma instância do ObjectMapper para processar JSON.
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cria uma instância do cliente S3 para interagir com o Amazon S3.
    private final S3Client s3Client = S3Client.builder().build();

    // Sobrescreve o metodo handleRequest, que é o ponto de entrada da função Lambda.
    @Override
    public Map<String, String> handleRequest(Map<String, Object> input, Context context) {
        // Obtém o corpo da requisição como uma String.
        String body = input.get("body").toString();

        // Declara um Map para armazenar o corpo da requisição como um mapa de chave-valor.
        Map<String, String> bodyMap;
        try {
            // Tenta converter o corpo da requisição (String) em um Map usando o ObjectMapper.
            bodyMap = objectMapper.readValue(body, Map.class);
        } catch (JsonProcessingException exception) {
            // Lança uma exceção se houver um erro ao analisar o JSON.
            throw new RuntimeException("Error parsing JSON body: " + exception.getMessage(), exception);
        }

        // Extrai a URL original e o tempo de expiração do mapa do corpo da requisição.
        String originalUrl = bodyMap.get("originalUrl");
        String expirationTime = bodyMap.get("expirationTime");
        // Converte o tempo de expiração de String para long (em segundos).
        long expirationTimeInSeconds = Long.parseLong(expirationTime);

        // Gera um código curto aleatório usando UUID.
        String shortUrlCode = UUID.randomUUID().toString().substring(0, 8);
        // Cria uma instância de UrlData para armazenar a URL original e o tempo de expiração.
        UrlData urlData = new UrlData(originalUrl, expirationTimeInSeconds);

        try {
            // Converte o objeto UrlData em uma String JSON.
            String urlDataJson = objectMapper.writeValueAsString(urlData);
            // Cria uma solicitação para armazenar o objeto no S3.
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket("miranda-ale-url-shortener") // Nome do bucket S3 onde os dados serão armazenados.
                    .key(shortUrlCode + ".json") // Nome do arquivo, que será o código curto seguido de .json.
                    .build();
            // Armazena a String JSON no bucket S3.
            s3Client.putObject(request, RequestBody.fromString(urlDataJson));
        } catch (Exception exception) {
            // Lança uma exceção se houver um erro ao salvar os dados no S3.
            throw new RuntimeException("Error saving URL data to S3: " + exception.getMessage(), exception);
        }

        // Cria um mapa para a resposta.
        Map<String, String> response = new HashMap<>();
        // Adiciona o código curto gerado ao mapa de resposta.
        response.put("code", shortUrlCode);
        // Retorna o mapa de resposta contendo o código curto.
        return response;
    }

}
