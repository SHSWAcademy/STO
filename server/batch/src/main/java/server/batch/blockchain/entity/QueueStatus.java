package server.batch.blockchain.entity;

public enum QueueStatus {
    PENDING,
    PROCESSING,
    SUBMITTED,
    CONFIRMED,
    FAILED,
    ABANDONED
}
