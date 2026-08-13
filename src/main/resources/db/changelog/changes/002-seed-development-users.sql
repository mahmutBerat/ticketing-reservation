--liquibase formatted sql

--changeset mbi:002-seed-development-users context:dev,test
INSERT INTO app_users (id, email, password_hash, created_at)
VALUES
    (1, 'admin@ticketing.local',
     '$2y$10$5qBWQu7SinWTd.A9AIG.vO2jJiboI1o4YOUgebV4Up4uRsP9jM.8a', CURRENT_TIMESTAMP),
    (2, 'organizer@ticketing.local',
     '$2y$10$5qBWQu7SinWTd.A9AIG.vO2jJiboI1o4YOUgebV4Up4uRsP9jM.8a', CURRENT_TIMESTAMP),
    (3, 'customer@ticketing.local',
     '$2y$10$5qBWQu7SinWTd.A9AIG.vO2jJiboI1o4YOUgebV4Up4uRsP9jM.8a', CURRENT_TIMESTAMP);

INSERT INTO user_roles (user_id, role)
VALUES
    (1, 'ADMIN'),
    (2, 'ORGANIZER'),
    (3, 'CUSTOMER');

--rollback DELETE FROM user_roles WHERE user_id IN (1, 2, 3);
--rollback DELETE FROM app_users WHERE id IN (1, 2, 3);
