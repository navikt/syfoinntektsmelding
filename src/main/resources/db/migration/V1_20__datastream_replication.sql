DO
$$
BEGIN
        if not exists
            (select 1 from pg_replication_slots where slot_name = 'spinosaurus_replication')
        then
            PERFORM PG_CREATE_LOGICAL_REPLICATION_SLOT ('spinosaurus_replication', 'pgoutput');
end if;
end;
$$;
