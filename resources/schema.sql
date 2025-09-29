-- Active: 1738622539335@@127.0.0.1@5432@crypto_wallet

CREATE TABLE IF NOT EXISTS wallets (
    id UUID PRIMARY KEY,
    type VARCHAR(20) NOT NULL,              
    address VARCHAR(100) UNIQUE NOT NULL,   
    password VARCHAR(100) NOT NULL,         
    balance DOUBLE PRECISION DEFAULT 0.0
);


CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID REFERENCES wallets(id) ON DELETE CASCADE,
    source_address VARCHAR(100) NOT NULL,
    destination_address VARCHAR(100) NOT NULL,
    amount DOUBLE PRECISION NOT NULL CHECK (amount > 0),
    fee DOUBLE PRECISION NOT NULL CHECK (fee >= 0),
    fee_level VARCHAR(20) NOT NULL,         
    status VARCHAR(20) NOT NULL,            
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);