DO
$$
BEGIN
        if not exists
            (select 1 from pg_publication where pubname = 'spinosaurus_publication')
        then
            CREATE PUBLICATION spinosaurus_publication for ALL TABLES;
end if;
end;
$$;
