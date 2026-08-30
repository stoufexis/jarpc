package stoufexis.jarpc.server;

import io.aeron.Image;
import org.agrona.concurrent.OneToOneConcurrentArrayQueue;

public final class Images {
  private static final int QUEUE_CAPACITY = 1024;

  private final OneToOneConcurrentArrayQueue<Image> availableImages =
      new OneToOneConcurrentArrayQueue<>(QUEUE_CAPACITY);

  private final OneToOneConcurrentArrayQueue<Image> unavailableImages =
      new OneToOneConcurrentArrayQueue<>(QUEUE_CAPACITY);

  public Image pollAvailable() {
    return availableImages.poll();
  }

  public Image pollUnavailable() {
    return unavailableImages.poll();
  }

  // FIXME need to figure out a more graceful error path
  public void enqueueAvailableImage(Image image) {
    if (!availableImages.offer(image)) {
      throw new RuntimeException("Unable to enqueue new image");
    }
  }

  public void enqueueUnavailableImage(Image image) {
    if (!unavailableImages.offer(image)) {
      throw new RuntimeException("Unable to enqueue removed image");
    }
  }
}
