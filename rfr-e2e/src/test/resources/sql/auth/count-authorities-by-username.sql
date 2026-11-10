select count(*)
from authority
where user_id in (select id from `user` where username = :username)
