package com.polywoof;

import lombok.extern.slf4j.Slf4j;
import org.h2.engine.Constants;
import org.h2.jdbcx.JdbcDataSource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@ParametersAreNonnullByDefault
public class PolywoofDB implements AutoCloseable
{
	private final Executor executor = Executors.newSingleThreadExecutor();
	private final JdbcDataSource data = new JdbcDataSource();
	private final String name;
	private Connection db;

	public PolywoofDB(String name, File file)
	{
		String path = file.getPath();

		if(path.endsWith(Constants.SUFFIX_MV_FILE))
			path = path.substring(0, path.length() - Constants.SUFFIX_MV_FILE.length());

		this.name = name;
		this.data.setURL(Constants.START_URL + path);
	}

	public void open(@Nullable Create callback)
	{
		if(status())
			return;

		executor.execute(() ->
		{
			try
			{
				Connection connection = data.getConnection();

				try(PreparedStatement statement = connection.prepareStatement(String.format("CREATE TABLE IF NOT EXISTS `%s` (RUNESCAPE VARCHAR(2048) PRIMARY KEY)", name)))
				{
					statement.executeUpdate();
					db = connection;

					if(callback != null)
						callback.create();
				}
				catch(SQLException error)
				{
					error.printStackTrace();
					connection.close();
				}
			}
			catch(SQLException error)
			{
				error.printStackTrace();
			}
		});
	}

	public void select(String entry, PolywoofTranslator.Language language, @Nullable Select callback)
	{
		if(!status())
			return;

		executor.execute(() ->
		{
			try(PreparedStatement statement = db.prepareStatement(String.format("SELECT `%2$s` FROM `%1$s` WHERE RUNESCAPE=? AND `%2$s` IS NOT NULL", name, language.code)))
			{
				statement.setString(1, entry);

				String string = null;
				ResultSet result = statement.executeQuery();

				if(result.next())
					string = result.getString(language.code);

				if(callback != null)
					callback.select(string);
			}
			catch(SQLException error)
			{
				error.printStackTrace();
			}
		});
	}

	public void insert(String entry, String string, PolywoofTranslator.Language language, @Nullable Create callback)
	{
		if(!status())
			return;

		executor.execute(() ->
		{
			try(PreparedStatement statement = db.prepareStatement(String.format("MERGE INTO `%1$s` (RUNESCAPE, `%2$s`) VALUES(?, ?)", name, language.code)))
			{
				statement.setString(1, entry);
				statement.setString(2, string);
				statement.executeUpdate();

				if(callback != null)
					callback.create();
			}
			catch(SQLException error)
			{
				error.printStackTrace();
			}
		});
	}

	public void update(List<PolywoofTranslator.Language> languages, @Nullable Update callback)
	{
		if(!status())
			return;

		executor.execute(() ->
		{
			for(PolywoofTranslator.Language language : languages)
			{
				try(PreparedStatement statement = db.prepareStatement(String.format("ALTER TABLE `%1$s` ADD IF NOT EXISTS `%2$s` VARCHAR(2048)", name, language.code)))
				{
					statement.executeUpdate();

					if(callback != null)
						callback.update(language);
				}
				catch(SQLException error)
				{
					error.printStackTrace();
				}
			}
		});
	}

	public void close()
	{
		if(!status())
			return;

		executor.execute(() ->
		{
			try
			{
				db.close();
			}
			catch(SQLException error)
			{
				error.printStackTrace();
			}
		});
	}

	public boolean status()
	{
		try
		{
			return db != null && !db.isClosed();
		}
		catch(SQLException error)
		{
			error.printStackTrace();
		}

		return false;
	}

	interface Create
	{
		void create();
	}

	interface Select
	{
		void select(@Nullable String string);
	}

	interface Update
	{
		void update(PolywoofTranslator.Language language);
	}
}
