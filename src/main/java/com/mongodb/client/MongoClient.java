package com.mongodb.client;

public interface MongoClient extends AutoCloseable {
    @Override
    void close();
}
