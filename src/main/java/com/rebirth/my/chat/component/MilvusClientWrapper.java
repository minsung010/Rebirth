package com.rebirth.my.chat.component;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.grpc.SearchResults;
import io.milvus.grpc.DataType;
import io.milvus.response.SearchResultsWrapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

@Component
public class MilvusClientWrapper {

    @Value("${milvus.host}")
    private String host;

    @Value("${milvus.port}")
    private int port;

    private MilvusServiceClient milvusClient;
    private static final String COLLECTION_NAME = "wardrobe_vector";
    private static final int VECTOR_DIM = 768; // Gemini Embedding Dimension

    @PostConstruct
    public void init() {
        try {
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port)
                    .build();
            milvusClient = new MilvusServiceClient(connectParam);
            System.out.println("✅ Milvus Connected: " + host + ":" + port);

            initCollection();
        } catch (Exception e) {
            System.err.println("❌ Milvus Connection Failed: " + e.getMessage());
            // Milvus failure shouldn't stop the whole app, but features will be disabled
        }
    }

    private void initCollection() {
        R<Boolean> hasCollection = milvusClient.hasCollection(
                HasCollectionParam.newBuilder().withCollectionName(COLLECTION_NAME).build());

        if (hasCollection.getData() == Boolean.FALSE) {
            System.out.println("Creating Milvus Collection: " + COLLECTION_NAME);

            FieldType idField = FieldType.newBuilder()
                    .withName("wardrobe_id")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();

            FieldType userIdField = FieldType.newBuilder()
                    .withName("user_id")
                    .withDataType(DataType.Int64)
                    .build();

            FieldType embeddingField = FieldType.newBuilder()
                    .withName("embedding")
                    .withDataType(DataType.FloatVector)
                    .withDimension(VECTOR_DIM)
                    .build();

            // Description for debug/verification
            FieldType descField = FieldType.newBuilder()
                    .withName("description")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(2000)
                    .build();

            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withDescription("Wardrobe Vector Data")
                    .addFieldType(idField)
                    .addFieldType(userIdField)
                    .addFieldType(embeddingField)
                    .addFieldType(descField)
                    .build();

            milvusClient.createCollection(createParam);
            System.out.println("✅ Milvus Collection Created");
        }
    }

    public void insertVector(Long wardrobeId, Long userId, List<Float> embedding, String description) {
        if (milvusClient == null)
            return;

        // Delete existing entry first (upsert pattern)
        try {
            milvusClient.delete(
                    io.milvus.param.dml.DeleteParam.newBuilder()
                            .withCollectionName(COLLECTION_NAME)
                            .withExpr("wardrobe_id == " + wardrobeId)
                            .build());
        } catch (Exception e) {
            // Ignore if doesn't exist
        }

        List<Long> wardrobeIds = new ArrayList<>();
        wardrobeIds.add(wardrobeId);

        List<Long> userIds = new ArrayList<>();
        userIds.add(userId);

        List<List<Float>> vectors = new ArrayList<>();
        vectors.add(embedding);

        List<String> descriptions = new ArrayList<>();
        descriptions.add(description);

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("wardrobe_id", wardrobeIds));
        fields.add(new InsertParam.Field("user_id", userIds));
        fields.add(new InsertParam.Field("embedding", vectors));
        fields.add(new InsertParam.Field("description", descriptions));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withFields(fields)
                .build();

        milvusClient.insert(insertParam);

        // Flush to ensure data is persisted and searchable
        milvusClient.flush(
                io.milvus.param.collection.FlushParam.newBuilder()
                        .addCollectionName(COLLECTION_NAME)
                        .build());

        System.out.println("✅ Inserted Vector for Wardrobe ID: " + wardrobeId);
    }

    public List<Long> searchSimilar(Long userId, List<Float> searchVector, int topK) {
        if (milvusClient == null)
            return Collections.emptyList();

        // Ensure collection is loaded
        milvusClient.loadCollection(
                io.milvus.param.collection.LoadCollectionParam.newBuilder()
                        .withCollectionName(COLLECTION_NAME)
                        .build());

        String expr = "user_id == " + userId; // Filter by User ID

        List<List<Float>> vectors = new ArrayList<>();
        vectors.add(searchVector);

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withMetricType(io.milvus.param.MetricType.COSINE)
                .withOutFields(Arrays.asList("wardrobe_id", "description"))
                .withTopK(topK)
                .withVectors(vectors)
                .withVectorFieldName("embedding")
                .withExpr(expr) // Partition by User
                .build();

        R<SearchResults> response = milvusClient.search(searchParam);
        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());

        List<Long> results = new ArrayList<>();
        List<SearchResultsWrapper.IDScore> idScores = wrapper.getIDScore(0);

        System.out.println("🔍 Search Results (Top " + topK + "):");
        for (SearchResultsWrapper.IDScore score : idScores) {
            System.out.println(" - ID: " + score.getLongID() + ", Score: " + score.getScore());
            results.add(score.getLongID());
        }

        return results;
    }
}
