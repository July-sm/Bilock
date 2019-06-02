package me.domin.bilock;

public interface TrainContract {
    interface View{
        void changeNum(int fileNum);
        void finishTrain();

    }
    interface Presenter{
        void trainData();
        void trainModel();
    }
}
