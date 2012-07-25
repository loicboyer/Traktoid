package com.florianmski.tracktoid.trakt.tasks;

import java.util.concurrent.ExecutorService;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.florianmski.tracktoid.Utils;
import com.florianmski.tracktoid.trakt.TraktManager;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.TraktException;

public abstract class BaseTask<TResult> extends BackgroundTask<TResult>
{
	protected final static int TOAST = -1;
	protected final static int EVENT = -2;
	protected final static int ERROR = -3;

	protected TraktManager tm;
	//this not will not display toast
	protected boolean silent = false;
	protected boolean silentConnectionError = false;
	protected boolean inQueue = false;
	protected Exception error;
	protected Context context;

	protected static ExecutorService sSingleThreadExecutor = new SingleThreadExecutor();
	
	public BaseTask(Context context) 
	{
		this(context, null);
	}
	
	public BaseTask(Context context, ExecutorService executor) 
	{
		super(executor);

		this.tm = TraktManager.getInstance();
		this.context = context;

		setId(this.getClass().getSimpleName());
	}

	@Override
	protected TResult doWorkInBackground() throws Exception 
	{
		if(!Utils.isOnline(context))
		{
			if(context != null && !silentConnectionError)
				onFailed(new Exception("Internet connection required!"));

			return doOfflineTraktStuff();
		}
		try
		{
			return doTraktStuffInBackground();
		}
		catch (ApiException e) 
		{
			onFailed(e);
			return null;
		}
		catch (TraktException e) 
		{
			onFailed(e);
			return null;
		}
		catch (IllegalArgumentException e) 
		{
			onFailed(e);
			return null;
		}
	}

	@Override
	protected void onCompleted(TResult result) 
	{
		if(result != null)
			sendEvent(result);
		
		Log.i("Traktoid","task ended : " + getId());
	}

	protected abstract void sendEvent(TResult result);

	@Override
	protected void onFailed(Exception e)
	{
		e.printStackTrace();
		this.error = e;
		this.publishProgress(ERROR, null);
		showToast("Error : " + e.getMessage(), Toast.LENGTH_LONG);
	}

	@Override
	protected void onPreExecute()
	{
		Log.i("Traktoid","start task : " + getId());
	}

	protected abstract TResult doTraktStuffInBackground();

	protected TResult doOfflineTraktStuff()
	{
		return null;
	}

	protected void showToast(String message, int duration)
	{
		if(!silent)
			publishProgress(TOAST, null, String.valueOf(duration), message);
	}

	@Override
	protected void onProgressPublished(int progress, TResult tmpResult, String... values)
	{
		if(progress == TOAST)
			Toast.makeText(context, values[1], Integer.parseInt(values[0])).show();
		else if(progress == ERROR)
		{
			//TODO something smart
		}
		else if(progress == EVENT)
		{
			if(tmpResult != null)
				sendEvent(tmpResult);
		}
	}

	public BaseTask<TResult> silent(boolean silent) 
	{
		this.silent = silent;
		return this;
	}

	//do nothin special in case of connection error (not even showing a toast)
	public BaseTask<TResult> silentConnectionError(boolean silentConnectionError) 
	{
		this.silentConnectionError = silentConnectionError;
		return this;
	}

	public void fire() 
	{
		execute();
	}
	
	public void detach()
	{
		context = null;
	}
	
	public void attach(Activity a)
	{
		context = a;
	}
}