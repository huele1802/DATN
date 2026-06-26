package com.example.AI.Hotel.service;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class JDBCTest {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://dpg-cvt561idbo4c73cicigg-a.oregon-postgres.render.com:5432/hotelproposal";
        String user = "huele";
        String password = "eOhfnkPnFeKhPeuGVtFZMsFIIIbc4joN";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT embedding FROM hotel_embeddings LIMIT 1")) {

            if (rs.next()) {
                Object embedding = rs.getObject("embedding");
                System.out.println("Embedding type: " + (embedding != null ? embedding.getClass().getName() : "null"));
                System.out.println("Embedding value: " + embedding);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
