package com.todoroo.astrid.adapter;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

import androidx.annotation.RequiresApi;
import com.todoroo.astrid.dao.TaskDao;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.SubsetCaldav;
import org.tasks.data.TaskContainer;
import org.tasks.tasklist.ViewHolder;

@RequiresApi(api = LOLLIPOP)
public final class CaldavTaskAdapter extends TaskAdapter {

  private final TaskDao taskDao;
  private final CaldavDao caldavDao;

  CaldavTaskAdapter(TaskDao taskDao, CaldavDao caldavDao) {
    this.taskDao = taskDao;
    this.caldavDao = caldavDao;
  }

  @Override
  public boolean canMove(ViewHolder sourceVh, ViewHolder targetVh) {
    TaskContainer source = sourceVh.task;
    int to = targetVh.getAdapterPosition();
    return !taskIsChild(source, to);
  }

  @Override
  public int maxIndent(int previousPosition, TaskContainer task) {
    TaskContainer previous = getTask(previousPosition);
    return previous.getIndent() + 1;
  }

  @Override
  public int minIndent(int nextPosition, TaskContainer task) {
    return 0;
  }

  @Override
  public boolean supportsParentingOrManualSort() {
    return true;
  }

  @Override
  public void moved(int from, int to, int indent) {
    TaskContainer task = getTask(from);
    TaskContainer previous = to > 0 ? getTask(to - 1) : null;

    long newParent = task.getParent();
    if (indent == 0) {
      newParent = 0;
    } else if (previous != null) {
      if (indent == previous.getIndent()) {
        newParent = previous.getParent();
      } else if (indent > previous.getIndent()) {
        newParent = previous.getId();
      } else if (indent < previous.getIndent()) {
        newParent = previous.getParent();
        int currentIndex = to;
        for (int i = 0 ; i < previous.getIndent() - indent ; i++) {
          long thisParent = newParent;
          while (newParent == thisParent) {
            thisParent = getTask(--currentIndex).getParent();
          }
          newParent = thisParent;
        }
      }
    }

    // If nothing is changing, return
    if (newParent == task.getParent()) {
      return;
    }

    changeParent(task, newParent);

    taskDao.touch(task.getId());
  }

  private void changeParent(TaskContainer task, long newParent) {
    SubsetCaldav caldavTask = task.getCaldavTask();

    if (newParent == 0) {
      caldavTask.setRemoteParent("");
      caldavTask.setParent(0);
    } else {
      CaldavTask parentTask = caldavDao.getTask(newParent);
      if (parentTask == null) {
        return;
      }
      caldavTask.setRemoteParent(parentTask.getRemoteId());
      caldavTask.setParent(newParent);
    }
    caldavDao.update(caldavTask);
  }

  private boolean taskIsChild(TaskContainer source, int destinationIndex) {
    TaskContainer destination = getTask(destinationIndex);
    while (destination.getParent() != 0) {
      if (destination.getParent() == source.getParent()) {
        return false;
      }
      if (destination.getParent() == source.getId()) {
        return true;
      }
      destination = getTask(--destinationIndex);
    }
    return false;
  }
}