CREATE TABLE roles
(
  guildid  BIGINT NOT NULL,
  roleid   BIGINT NOT NULL,
  rolename VARCHAR(21845),
  CONSTRAINT roles_guildid_roleid_pk
  PRIMARY KEY (guildid, roleid)
);

CREATE TABLE registered_emoji_server
(
  guildid BIGINT      NOT NULL,
  title   VARCHAR(10) NOT NULL,
  CONSTRAINT registered_emoji_server_pkey
  PRIMARY KEY (guildid)
);

CREATE UNIQUE INDEX registered_emoji_server_guildid_uindex
  ON registered_emoji_server (guildid);

CREATE UNIQUE INDEX registered_emoji_server_title_uindex
  ON registered_emoji_server (title);

CREATE TABLE disabled_emoji_servers
(
  guildid       BIGINT NOT NULL,
  emoji_guildid BIGINT NOT NULL,
  CONSTRAINT disabled_emoji_servers_guildid_emoji_guildid_pk
  PRIMARY KEY (guildid, emoji_guildid)
);


