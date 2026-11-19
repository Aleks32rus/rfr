select count(*)
from friendship
where status = 'PENDING'
  and addressee_id in (select id from `user` where username = :username)
