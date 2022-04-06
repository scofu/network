package com.scofu.network.document.api;

/**
 * Published by the document service when a document update has been acknowledged by the database.
 *
 * @param collection the collection
 * @param json       the json
 */
public record DocumentUpdatedMessage(String collection, String json) {}