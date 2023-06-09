package com.nctigba.observability.sql.service.diagnosis;

import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.opengauss.admin.common.exception.CustomException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.nctigba.observability.sql.mapper.DiagnosisTaskMapper;
import com.nctigba.observability.sql.model.diagnosis.Task;
import com.nctigba.observability.sql.model.diagnosis.TaskState;
import com.nctigba.observability.sql.service.diagnosis.caller.Caller;

import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskCreator {
	public static final Semaphore SEMAPHORE = new Semaphore(1);
	private final DiagnosisTaskMapper mapper;
	private final SqlExecuter nextStep;
	private final List<Caller> callers;
	private final static Queue<Task> queue = new ConcurrentLinkedQueue<Task>();
	private static Task task;

	public void createTask(Task task) {
		queue.add(task);
	}

	public void remove(Integer id) {
		for (Task task : queue)
			if(task.getId()!=null && id.equals(task.getId())) {
				queue.remove(task);
				break;
			}
	}

	@Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
	public void run() {
		while (true)
			if (task == null || TaskState.receiving.compareTo(task.getState()) < 0) {
				task = queue.poll();
				if(task != null)
					break;
			} else
				ThreadUtil.sleep(1000);
		task.addRemarks("before execute");
		mapper.updateById(task);
		for (Caller caller : callers)
			try {
				caller.beforeStart(task);
			} catch (CustomException e) {
				mapper.updateById(task);
				return;
			} catch (Exception e) {
				task.addRemarks(TaskState.err, e);
				mapper.updateById(task);
				return;
			}
		// call sql and fetch pid
		task.addRemarks(TaskState.waiting);
		mapper.updateById(task);
		try {
			SEMAPHORE.acquire();
		} catch (InterruptedException e) {
			throw new CustomException("", e);
		}
		task.setStarttime(new Date());
		mapper.updateById(task);
		nextStep.executeSql(task);
		for (Caller caller : callers) {
			caller.start(task);
		}
	}
}