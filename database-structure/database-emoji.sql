-- we don't know how to generate database emoji-er (class Database) :(
create table guilds
(
  guildid   bigint                not null,
  guildname varchar(1000),
  enabled   boolean default true not null,
  constraint guilds_pkey
  primary key (guildid)
);

create table roles
(
  guildid  bigint not null,
  roleid   bigint not null,
  rolename varchar(21845),
  constraint roles_1_guildid_roleid_pk
  primary key (guildid, roleid),
  constraint roles_1_guilds_guildid_fk
  foreign key (guildid) references guilds
);

create table registered_emoji_server
(
  guildid bigint not null,
  title   varchar(10),
  constraint registered_emoji_server_pkey
  primary key (guildid),
  constraint registered_emoji_server_guilds_guildid_fk
  foreign key (guildid) references guilds
);

create table disabled_emoji_servers
(
  guildid       bigint not null,
  emoji_guildid bigint not null,
  constraint disabled_emoji_servers_guildid_emoji_guildid_pk
  primary key (guildid, emoji_guildid),
  constraint disabled_emoji_servers_guilds_guildid_fk
  foreign key (guildid) references guilds,
  constraint disabled_emoji_servers_registered_emoji_server_guildid_fk
  foreign key (emoji_guildid) references registered_emoji_server
);


