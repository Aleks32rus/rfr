select count(distinct authority)
from authority
where user_id in (select id from `user` where username = :username)
  and authority in ('read', 'write')
