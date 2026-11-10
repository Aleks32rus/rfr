insert into authority (id, user_id, authority)
values (UUID_TO_BIN(:id), UUID_TO_BIN(:userId), :authority)
