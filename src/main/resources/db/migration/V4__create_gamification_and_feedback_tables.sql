CREATE TABLE user_gamifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    level INTEGER NOT NULL DEFAULT 1,
    exp INTEGER NOT NULL DEFAULT 0,
    title VARCHAR(100) NOT NULL DEFAULT '독자',
    attendance_streak INTEGER NOT NULL DEFAULT 0,
    last_attendance_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_gamifications_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_gamifications_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE work_feedbacks (
    id UUID PRIMARY KEY,
    work_id UUID NOT NULL,
    user_id UUID NOT NULL,
    feedback_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_work_feedbacks_user_work_type UNIQUE (user_id, work_id, feedback_type),
    CONSTRAINT fk_work_feedbacks_work_id FOREIGN KEY (work_id) REFERENCES works(id) ON DELETE CASCADE,
    CONSTRAINT fk_work_feedbacks_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
