package com.polywoof;

import lombok.extern.slf4j.Slf4j;
import org.h2.jdbcx.JdbcDataSource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
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
	private final String table;
	private Connection db;

	public PolywoofDB(String table, String filename)
	{
		this.table = table;

		this.data.setURL("jdbc:h2:" + filename);
	}

	public void open(Result callback)
	{
		if(status())
			return;

		executor.execute(() ->
		{
			try
			{
				Connection connection = data.getConnection();

				try(PreparedStatement statement = connection.prepareStatement(String.format("CREATE TABLE IF NOT EXISTS `%s` (RUNESCAPE VARCHAR(2048) PRIMARY KEY)", table)))
				{
					statement.executeUpdate();

					db = connection;

					callback.result(null);
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

	public void select(String text, PolywoofTranslator.Language language, Result callback)
	{
		if(!status())
			return;

		executor.execute(() ->
		{
			try(PreparedStatement statement = db.prepareStatement(String.format("SELECT `%2$s` FROM `%1$s` WHERE RUNESCAPE=? AND `%2$s` IS NOT NULL", table, language.code)))
			{
				statement.setString(1, text);

				String string = null;
				ResultSet result = statement.executeQuery();

				if(result.next())
					string = result.getString(language.code);

				callback.result(string);
			}
			catch(SQLException error)
			{
				error.printStackTrace();
			}
		});
	}

	public void insert(String text, String string, PolywoofTranslator.Language language)
	{
		if(!status())
			return;

		executor.execute(() ->
		{
			try(PreparedStatement statement = db.prepareStatement(String.format("MERGE INTO `%1$s` (RUNESCAPE, `%2$s`) VALUES(?, ?)", table, language.code)))
			{
				statement.setString(1, text);
				statement.setString(2, string);
				statement.executeUpdate();
			}
			catch(SQLException error)
			{
				error.printStackTrace();
			}
		});
	}

	public void update(List<PolywoofTranslator.Language> languages)
	{
		if(!status())
			return;

		executor.execute(() ->
		{
			for(PolywoofTranslator.Language language : languages)
			{
				try(PreparedStatement statement = db.prepareStatement(String.format("ALTER TABLE `%1$s` ADD IF NOT EXISTS `%2$s` VARCHAR(2048)", table, language.code)))
				{
					statement.executeUpdate();
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
				db.rollback();
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

	interface Result
	{
		void result(@Nullable String string);
	}
}
