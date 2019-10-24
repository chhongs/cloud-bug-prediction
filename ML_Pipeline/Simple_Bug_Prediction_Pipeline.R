install.packages("RWeka")
install.packages("stringr")
install.packages("partykit")

library(stringr)
library(RWeka)
library(partykit)


pathTrainingSet<-paste("Prediction/log4j-1.1.csv",sep="")
pathTestSet<-paste("Prediction/log4j-1.2.csv",sep="")

#retrieve training set
trainingSet<-read.csv(pathTrainingSet)

#retrieve testing set
testSet<-read.csv(pathTestSet)

#names(trainingSet)
#attach(trainingSet)

#logistic regression model
model <- Logistic(bug~wmc+dit+noc+cbo+lcom+ca+ce+lcom3+loc, data = trainingSet) #Logistic builds multinomial logistic regression models based on ridge estimation
#model<- J48(bug~wmc+dit+noc+cbo+lcom+ca+ce+lcom3+loc, data = trainingSet)
summary(model)
#plot(model)
# Use 10 fold cross-validation. - evaluate model using training data
#e <- evaluate_Weka_classifier(model,cost = matrix(c(0,2,1,0), ncol = 2),numFolds = 10, complexity = TRUE,sed = 123, class = TRUE)
#confusionMatrix<-e$confusionMatrix

#confusionMatrix

# Use 10 fold cross-validation. - evaluate model using test data
e2 <- evaluate_Weka_classifier(model,newdata = testSet,cost = matrix(c(0,2,1,0), ncol = 2),numFolds = 10, complexity = TRUE,sed = 123, class = TRUE)
confusionMatrix<-e2$confusionMatrix

confusionMatrix

#precision doesnt mirror the inspection cost. need cost oriented models
precision <-confusionMatrix[2,2]/sum(confusionMatrix[,2])#PRECISION...
precision
recall<-confusionMatrix[2,2]/sum(confusionMatrix[2,])#Recall...
recall
accuracy<-sum(confusionMatrix[1,1],confusionMatrix[2,2])/sum(confusionMatrix[,])
accuracy
false_positive_rate <- confusionMatrix[2,1]/sum(confusionMatrix[1,])#false positive rate
false_positive_rate
## S3 method for class 'Weka_classifier'
#predict <- predict(model, newdata = testSet,type = c("class", "probability"))
#predict
