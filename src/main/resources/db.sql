create table delivery_order
(
    order_id int unsigned auto_increment,
    sender_name varchar(16) null,
    sender_uuid varchar(36) null,
    receiver_name varchar(16) null,
    receiver_uuid varchar(36) null,
    order_date DATETIME null,
    receive_date DATETIME null,
    order_status boolean default false,
    wrapping varchar(16) default 'cardboard',
    boxName varchar(50) default '段ボール箱',
    amount tinyint not null,
    slot1 longtext null,
    slot2 longtext null,
    slot3 longtext null,
    slot4 longtext null,
    slot5 longtext null,
    slot6 longtext null,
    slot7 longtext null,
    slot8 longtext null,
    box_status boolean default false,
    opener_name varchar(16) null,
    opener_uuid varchar(36) null,
    opened_date DATETIME null,

    primary key(order_id)
);

create table player_status
(
    player_id int unsigned auto_increment,
    owner_name varchar(16) null,
    owner_uuid varchar(36) not null,
    delivery_amount int default 0,

    primary key(player_id)
);

create index delivery_order_sender_uuid_index on delivery_order(sender_uuid);

create index delivery_order_receiver_uuid_index on delivery_order(receiver_uuid);

create index player_status_owner_uuid_index on player_status(owner_uuid);

create index player_status_owner_name_index on player_status(owner_name);