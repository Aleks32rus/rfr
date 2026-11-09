select count(*)
from friendship
where requester_id = UUID_TO_BIN(:id)
   or addressee_id = UUID_TO_BIN(:id)
   or requester_id in (select id from `user` where username = :username)
   or addressee_id in (select id from `user` where username = :username)
