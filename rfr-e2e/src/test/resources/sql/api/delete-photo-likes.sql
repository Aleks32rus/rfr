delete relation from photo_like relation
join photo p on p.id = relation.photo_id
join `like` l on l.id = relation.like_id
where p.user_id in (select id from `user` where username = :username)
   or l.user_id in (select id from `user` where username = :username)
