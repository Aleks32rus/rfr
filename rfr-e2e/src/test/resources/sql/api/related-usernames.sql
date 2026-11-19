select distinct related.username
from `user` owner
         join friendship relation on owner.id = relation.requester_id or owner.id = relation.addressee_id
         join `user` related on related.id = relation.requester_id or related.id = relation.addressee_id
where owner.username = :username
