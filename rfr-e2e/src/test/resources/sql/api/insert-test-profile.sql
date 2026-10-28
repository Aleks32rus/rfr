insert into `user` (id, username, country_id)
select UUID_TO_BIN(:id), :username, id from country where code = 'ru' limit 1
