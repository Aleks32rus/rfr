select count(*)
from friendship
where status = :status
  and (requester_id in (select id from `user` where username = :username)
    or addressee_id in (select id from `user` where username = :username))
