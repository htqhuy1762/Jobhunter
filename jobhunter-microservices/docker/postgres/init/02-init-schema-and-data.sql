-- =====================================================
-- MICROSERVICES SCHEMA AND DATA INITIALIZATION (PostgreSQL)
-- =====================================================
-- This script creates tables and inserts sample data for PostgreSQL
-- Run order: 02 (runs after 01-init-databases.sql)
-- =====================================================

-- ========================================
-- AUTH_DB - Authentication Service
-- ========================================
\c auth_db;

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    age INT,
    gender VARCHAR(50),
    address VARCHAR(500),
    company_id BIGINT,
    role_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);

-- Permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    api_path VARCHAR(255),
    method VARCHAR(10),
    module VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_permissions_name ON permissions(name);

-- Role-Permission junction table
CREATE TABLE IF NOT EXISTS permission_role (
    permission_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (permission_id, role_id),
    CONSTRAINT fk_pr_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Skills table (Read-only cache/replica of job_db.skills)
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_skills_name ON skills(name);

-- Subscribers table (Job alert subscriptions)
CREATE TABLE IF NOT EXISTS subscribers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uk_subscribers_email UNIQUE (email),
    CONSTRAINT uk_subscribers_user_id UNIQUE (user_id),
    CONSTRAINT fk_subscribers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_subscribers_email ON subscribers(email);
CREATE INDEX IF NOT EXISTS idx_subscribers_active ON subscribers(active);

-- Subscriber-Skill junction table
CREATE TABLE IF NOT EXISTS subscriber_skill (
    subscriber_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (subscriber_id, skill_id),
    CONSTRAINT fk_ss_subscriber FOREIGN KEY (subscriber_id) REFERENCES subscribers(id) ON DELETE CASCADE,
    CONSTRAINT fk_ss_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

-- Insert default roles
INSERT INTO roles (name, description, active, created_by) VALUES
('ROLE_ADMIN', 'Administrator with full access', TRUE, 'system'),
('ROLE_USER', 'Regular user with limited access', TRUE, 'system'),
('ROLE_HR', 'HR manager', TRUE, 'system')
ON CONFLICT (name) DO NOTHING;

-- Insert sample skills (Read-only cache from job_db)
INSERT INTO skills (id, name, created_by) VALUES
(1, 'Java', 'system'),
(2, 'Spring Boot', 'system'),
(3, 'JavaScript', 'system'),
(4, 'React', 'system'),
(5, 'Angular', 'system'),
(6, 'Vue.js', 'system'),
(7, 'Node.js', 'system'),
(8, 'Python', 'system'),
(9, 'Django', 'system'),
(10, 'MySQL', 'system'),
(11, 'PostgreSQL', 'system'),
(12, 'MongoDB', 'system'),
(13, 'Docker', 'system'),
(14, 'Kubernetes', 'system'),
(15, 'AWS', 'system'),
(16, 'Azure', 'system'),
(17, 'Git', 'system'),
(18, 'CI/CD', 'system'),
(19, 'Microservices', 'system'),
(20, 'REST API', 'system')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- NOTE: Users are seeded by application code!
-- ============================================

-- Insert comprehensive permissions for all modules
INSERT INTO permissions (name, api_path, method, module, created_by) VALUES
-- User Management (ADMIN only)
('CREATE_USER', '/api/v1/users', 'POST', 'USER', 'system'),
('UPDATE_USER', '/api/v1/users/*', 'PUT', 'USER', 'system'),
('DELETE_USER', '/api/v1/users/*', 'DELETE', 'USER', 'system'),
('VIEW_USER', '/api/v1/users', 'GET', 'USER', 'system'),

-- Role Management (ADMIN only)
('CREATE_ROLE', '/api/v1/roles', 'POST', 'ROLE', 'system'),
('UPDATE_ROLE', '/api/v1/roles/*', 'PUT', 'ROLE', 'system'),
('DELETE_ROLE', '/api/v1/roles/*', 'DELETE', 'ROLE', 'system'),
('VIEW_ROLE', '/api/v1/roles', 'GET', 'ROLE', 'system'),

-- Permission Management (ADMIN only)
('CREATE_PERMISSION', '/api/v1/permissions', 'POST', 'PERMISSION', 'system'),
('UPDATE_PERMISSION', '/api/v1/permissions/*', 'PUT', 'PERMISSION', 'system'),
('DELETE_PERMISSION', '/api/v1/permissions/*', 'DELETE', 'PERMISSION', 'system'),
('VIEW_PERMISSION', '/api/v1/permissions', 'GET', 'PERMISSION', 'system'),

-- Company Management
('CREATE_COMPANY', '/api/v1/companies', 'POST', 'COMPANY', 'system'),
('UPDATE_COMPANY', '/api/v1/companies/*', 'PUT', 'COMPANY', 'system'),
('DELETE_COMPANY', '/api/v1/companies/*', 'DELETE', 'COMPANY', 'system'),
('VIEW_COMPANY', '/api/v1/companies', 'GET', 'COMPANY', 'system'),

-- Job Management
('CREATE_JOB', '/api/v1/jobs', 'POST', 'JOB', 'system'),
('UPDATE_JOB', '/api/v1/jobs/*', 'PUT', 'JOB', 'system'),
('DELETE_JOB', '/api/v1/jobs/*', 'DELETE', 'JOB', 'system'),
('VIEW_JOB', '/api/v1/jobs', 'GET', 'JOB', 'system'),

-- Skill Management (ADMIN only)
('CREATE_SKILL', '/api/v1/skills', 'POST', 'SKILL', 'system'),
('UPDATE_SKILL', '/api/v1/skills/*', 'PUT', 'SKILL', 'system'),
('DELETE_SKILL', '/api/v1/skills/*', 'DELETE', 'SKILL', 'system'),
('VIEW_SKILL', '/api/v1/skills', 'GET', 'SKILL', 'system'),

-- Resume Management
('CREATE_RESUME', '/api/v1/resumes', 'POST', 'RESUME', 'system'),
('UPDATE_RESUME', '/api/v1/resumes/*', 'PUT', 'RESUME', 'system'),
('DELETE_RESUME', '/api/v1/resumes/*', 'DELETE', 'RESUME', 'system'),
('VIEW_ALL_RESUME', '/api/v1/resumes', 'GET', 'RESUME', 'system'),
('VIEW_OWN_RESUME', '/api/v1/resumes/by-user', 'GET', 'RESUME', 'system'),

-- File Management
('UPLOAD_FILE', '/api/v1/files', 'POST', 'FILE', 'system'),
('DOWNLOAD_FILE', '/api/v1/files', 'GET', 'FILE', 'system'),

-- Subscriber Management
('CREATE_SUBSCRIBER', '/api/v1/subscribers', 'POST', 'SUBSCRIBER', 'system'),
('UPDATE_SUBSCRIBER', '/api/v1/subscribers', 'PUT', 'SUBSCRIBER', 'system'),
('DELETE_SUBSCRIBER', '/api/v1/subscribers/*', 'DELETE', 'SUBSCRIBER', 'system'),
('VIEW_SUBSCRIBER', '/api/v1/subscribers', 'GET', 'SUBSCRIBER', 'system')
ON CONFLICT (name) DO NOTHING;

-- ==========================================
-- ASSIGN PERMISSIONS TO ROLE_ADMIN (All permissions)
-- ==========================================
INSERT INTO permission_role (permission_id, role_id)
SELECT p.id, r.id
FROM permissions p
CROSS JOIN roles r
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- ==========================================
-- ASSIGN PERMISSIONS TO ROLE_HR
-- ==========================================
INSERT INTO permission_role (permission_id, role_id)
SELECT p.id, r.id
FROM permissions p
CROSS JOIN roles r
WHERE r.name = 'ROLE_HR'
AND p.name IN (
    'VIEW_USER',
    'VIEW_COMPANY',
    'CREATE_COMPANY',
    'UPDATE_COMPANY',
    'VIEW_JOB',
    'CREATE_JOB',
    'UPDATE_JOB',
    'DELETE_JOB',
    'VIEW_SKILL',
    'VIEW_ALL_RESUME',
    'UPDATE_RESUME',
    'DELETE_RESUME',
    'UPLOAD_FILE',
    'DOWNLOAD_FILE'
)
ON CONFLICT DO NOTHING;

-- ==========================================
-- ASSIGN PERMISSIONS TO ROLE_USER
-- ==========================================
INSERT INTO permission_role (permission_id, role_id)
SELECT p.id, r.id
FROM permissions p
CROSS JOIN roles r
WHERE r.name = 'ROLE_USER'
AND p.name IN (
    'VIEW_COMPANY',
    'VIEW_JOB',
    'VIEW_SKILL',
    'CREATE_RESUME',
    'VIEW_OWN_RESUME',
    'DELETE_RESUME',
    'UPLOAD_FILE',
    'DOWNLOAD_FILE',
    'CREATE_SUBSCRIBER',
    'UPDATE_SUBSCRIBER',
    'DELETE_SUBSCRIBER'
)
ON CONFLICT DO NOTHING;

-- ========================================
-- COMPANY_DB - Company Service
-- ========================================
\c company_db;

CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address VARCHAR(500),
    logo VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_companies_name ON companies(name);

-- ============================================
-- NOTE: Sample companies are seeded by application code!
-- ============================================

-- ========================================
-- JOB_DB - Job Service
-- ========================================
\c job_db;

CREATE TABLE IF NOT EXISTS jobs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255)NOT NULL,
    location VARCHAR(50) DEFAULT 'OTHER',
    salary DECIMAL(15,2),
    quantity INT,
    level VARCHAR(50),
    description TEXT,
    start_date DATE,
    end_date DATE,
    active BOOLEAN DEFAULT TRUE,
    company_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_jobs_company_id ON jobs(company_id);
CREATE INDEX IF NOT EXISTS idx_jobs_level ON jobs(level);
CREATE INDEX IF NOT EXISTS idx_jobs_active ON jobs(active);
CREATE INDEX IF NOT EXISTS idx_jobs_location ON jobs(location);

CREATE TABLE IF NOT EXISTS skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_skills_name ON skills(name);

CREATE TABLE IF NOT EXISTS job_skill (
    job_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (job_id, skill_id),
    CONSTRAINT fk_js_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_js_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

-- Insert sample skills
INSERT INTO skills (name, created_by) VALUES
('Java', 'system'),
('Spring Boot', 'system'),
('JavaScript', 'system'),
('React', 'system'),
('Angular', 'system'),
('Vue.js', 'system'),
('Node.js', 'system'),
('Python', 'system'),
('Django', 'system'),
('MySQL', 'system'),
('PostgreSQL', 'system'),
('MongoDB', 'system'),
('Docker', 'system'),
('Kubernetes', 'system'),
('AWS', 'system'),
('Azure', 'system'),
('Git', 'system'),
('CI/CD', 'system'),
('Microservices', 'system'),
('REST API', 'system')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- NOTE: Sample jobs are seeded by application code!
-- ============================================

-- ========================================
-- RESUME_DB - Resume Service
-- ========================================
\c resume_db;

CREATE TABLE IF NOT EXISTS resumes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    user_id BIGINT,
    job_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_resumes_user_id ON resumes(user_id);
CREATE INDEX IF NOT EXISTS idx_resumes_job_id ON resumes(job_id);
CREATE INDEX IF NOT EXISTS idx_resumes_status ON resumes(status);

-- ========================================
-- FILE_DB - File Service (Optional metadata)
-- ========================================
\c file_db;

CREATE TABLE IF NOT EXISTS file_metadata (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255),
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    content_type VARCHAR(100),
    bucket_name VARCHAR(100),
    uploaded_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_file_metadata_file_name ON file_metadata(file_name);
CREATE INDEX IF NOT EXISTS idx_file_metadata_uploaded_by ON file_metadata(uploaded_by);

-- ========================================
-- SUMMARY
-- ========================================
\c postgres;
SELECT '============================================' AS message;
SELECT 'ALL MICROSERVICES DATABASES INITIALIZED!' AS message;
SELECT '============================================' AS message;

