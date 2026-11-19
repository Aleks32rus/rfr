insert into friendship (id, requester_id, addressee_id, status, created_at, version)
values (UUID_TO_BIN(:id), UUID_TO_BIN(:requesterId), UUID_TO_BIN(:addresseeId),
        'PENDING', CURRENT_TIMESTAMP(6), 0)
