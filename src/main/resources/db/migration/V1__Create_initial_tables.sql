-- Create sports table
CREATE TABLE sports (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    logo_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create teams table
CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) UNIQUE,
    logo_url VARCHAR(500),
    country VARCHAR(100),
    founded_year VARCHAR(10),
    sport_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sport_id) REFERENCES sports(id) ON DELETE CASCADE
);

-- Create competitions table
CREATE TABLE competitions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) UNIQUE,
    category VARCHAR(100),
    season VARCHAR(20),
    logo_url VARCHAR(500),
    sport_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sport_id) REFERENCES sports(id) ON DELETE CASCADE
);

-- Create matches table
CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(100) UNIQUE,
    home_team_id BIGINT NOT NULL,
    away_team_id BIGINT NOT NULL,
    competition_id BIGINT,
    sport_id BIGINT NOT NULL,
    match_date TIMESTAMP NOT NULL,
    status VARCHAR(50),
    home_score INTEGER,
    away_score INTEGER,
    current_minute VARCHAR(10),
    venue VARCHAR(200),
    city VARCHAR(100),
    country VARCHAR(100),
    match_events TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (home_team_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (away_team_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (competition_id) REFERENCES competitions(id) ON DELETE CASCADE,
    FOREIGN KEY (sport_id) REFERENCES sports(id) ON DELETE CASCADE
);

-- Create news table
CREATE TABLE news (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(1000) NOT NULL,
    content TEXT,
    summary TEXT,
    author VARCHAR(200),
    source VARCHAR(200),
    image_url VARCHAR(1000),
    article_url VARCHAR(1000),
    sport_id BIGINT,
    team_id BIGINT,
    competition_id BIGINT,
    published_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sport_id) REFERENCES sports(id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (competition_id) REFERENCES competitions(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_sports_code ON sports(code);
CREATE INDEX idx_teams_sport_id ON teams(sport_id);
CREATE INDEX idx_teams_name ON teams(name);
CREATE INDEX idx_competitions_sport_id ON competitions(sport_id);
CREATE INDEX idx_matches_sport_id ON matches(sport_id);
CREATE INDEX idx_matches_date ON matches(match_date);
CREATE INDEX idx_matches_status ON matches(status);
CREATE INDEX idx_matches_home_team ON matches(home_team_id);
CREATE INDEX idx_matches_away_team ON matches(away_team_id);
CREATE INDEX idx_news_sport_id ON news(sport_id);
CREATE INDEX idx_news_published_at ON news(published_at);
CREATE INDEX idx_news_team_id ON news(team_id);

-- Insert default sports
INSERT INTO sports (name, code, description) VALUES
('Football', 'football', 'Actualités et matchs de football'),
('Basketball', 'basketball', 'Actualités et matchs de basketball'),
('Tennis', 'tennis', 'Actualités et matchs de tennis'),
('Hockey sur glace', 'hockey', 'Actualités et matchs de hockey sur glace'),
('Rugby', 'rugby', 'Actualités et matchs de rugby'),
('Cyclisme', 'cycling', 'Actualités et compétitions de cyclisme'),
('Formule 1', 'formula1', 'Actualités et courses de Formule 1'),
('Judo', 'judo', 'Actualités et compétitions de judo'),
('Natation', 'swimming', 'Actualités et compétitions de natation');
