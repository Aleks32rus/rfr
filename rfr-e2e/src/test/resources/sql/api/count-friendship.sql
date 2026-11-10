select count(*)
from friendship
where status = :status
  and ((requester_id = UUID_TO_BIN(:owner) and addressee_id = UUID_TO_BIN(:peer))
    or (:status = 'ACCEPTED' and requester_id = UUID_TO_BIN(:peer) and addressee_id = UUID_TO_BIN(:owner)))
