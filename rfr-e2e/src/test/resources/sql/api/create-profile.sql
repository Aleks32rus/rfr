insert into `user` (id, username, country_id)
values (UUID_TO_BIN(:id), :username, (select id from country where code = :countryCode))
