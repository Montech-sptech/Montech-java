package com.montech.s3;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    public S3Service(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public List<String> listarBuckets() {
        List<String> nomes = new ArrayList<>();
        for (Bucket b : s3Client.listBuckets().buckets()) {
            nomes.add(b.name());
        }
        return nomes;
    }

    public List<String> listarCsvs(String prefix) {
        List<String> chaves = new ArrayList<>();
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        for (ListObjectsV2Response page : s3Client.listObjectsV2Paginator(request)) {
            for (S3Object obj : page.contents()) {
                if (obj.key().endsWith(".csv")) {
                    chaves.add(obj.key());
                }
            }
        }
        return chaves;
    }

    public List<String[]> lerCsv(String key) throws IOException {
        List<String[]> linhas = new ArrayList<>();
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(request);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String linha;
            while ((linha = reader.readLine()) != null) {
                linhas.add(linha.split(","));
            }
        }
        return linhas;
    }

    public List<String> listarPorExtensao(String prefix, String extensao) {
        List<String> chaves = new ArrayList<>();
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        for (ListObjectsV2Response page : s3Client.listObjectsV2Paginator(request)) {
            for (S3Object obj : page.contents()) {
                if (obj.key().endsWith(extensao)) {
                    chaves.add(obj.key());
                }
            }
        }
        return chaves;
    }

    public String lerTexto(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObjectAsBytes(request).asUtf8String();
    }
}