-- =====================================================
-- MICROSERVICES DATABASE INITIALIZATION (PostgreSQL)
-- =====================================================
-- This script creates separate databases for each microservice
-- Run order: 01 (runs first automatically by Docker)
-- =====================================================

-- Create databases
CREATE DATABASE auth_db;
CREATE DATABASE company_db;
CREATE DATABASE job_db;
CREATE DATABASE resume_db;
CREATE DATABASE file_db;

-- Note: PostgreSQL grants all privileges to the owner automatically
-- The 'postgres' user already has full access to all databases

SELECT 'Databases created successfully!' AS message;

