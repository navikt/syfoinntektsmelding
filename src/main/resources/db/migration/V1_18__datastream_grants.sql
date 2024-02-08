DO
$$
BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'spinosaurus')
        THEN
            ALTER USER "spinosaurus" WITH REPLICATION;
END IF;
END
$$;
DO
$$
BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'spinosaurus_datastream_bruker')
        THEN
            ALTER USER "spinosaurus_datastream_bruker" WITH REPLICATION;
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO "spinosaurus_datastream_bruker";
GRANT USAGE ON SCHEMA public TO "spinosaurus_datastream_bruker";
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO "spinosaurus_datastream_bruker";
END IF;
END
$$;
