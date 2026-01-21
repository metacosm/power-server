
    create table Measure (
        id bigint not null,
        appName varchar(255),
        components blob,
        duration bigint not null,
        endTime bigint not null,
        externalCPUShare float not null,
        session varchar(255),
        startTime bigint not null,
        primary key (id)
    );

    create table Measure_SEQ (
        next_val bigint
    );

    insert into Measure_SEQ ( next_val ) values ( 1 );
