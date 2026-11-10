delete from authority
where user_id = UUID_TO_BIN(:userId)
  and authority = :authority
