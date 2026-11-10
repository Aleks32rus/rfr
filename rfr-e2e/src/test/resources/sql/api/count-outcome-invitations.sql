select count(*)
from friendship
where status = 'PENDING'
  and requester_id in (select id from `user` where username = :username)
