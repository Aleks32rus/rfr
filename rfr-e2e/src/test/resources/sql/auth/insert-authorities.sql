insert into authority (id, user_id, authority)
values (UUID_TO_BIN(:readId), UUID_TO_BIN(:userId), 'read'),
       (UUID_TO_BIN(:writeId), UUID_TO_BIN(:userId), 'write')
